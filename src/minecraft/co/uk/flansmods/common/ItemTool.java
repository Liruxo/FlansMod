package co.uk.flansmods.common;

import java.util.List;

import co.uk.flansmods.client.debug.EntityDebugVector;
import co.uk.flansmods.common.driveables.DriveableData;
import co.uk.flansmods.common.driveables.DriveablePart;
import co.uk.flansmods.common.driveables.EntityDriveable;
import co.uk.flansmods.common.driveables.EntityPlane;
import co.uk.flansmods.common.driveables.PlaneType;
import co.uk.flansmods.common.vector.Vector3f;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ItemTool extends Item 
{
	public ToolType type;

    public ItemTool(int i, ToolType t)
    {
        super(i);
        maxStackSize = 1;
		type = t;
		type.item = this;
		setMaxDamage(type.toolLife);
		setCreativeTab(FlansMod.tabFlanParts);
    }
    
	@Override
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advancedTooltips) 
	{
		
	}
    
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack par1ItemStack, int par2)
    {
    	return type.colour;
    }
	
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IconRegister icon) 
    {
    	itemIcon = icon.registerIcon("FlansMod:" + type.iconPath);
    }
    
	@Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer)
    {
    	//Raytracing
        float cosYaw = MathHelper.cos(-entityplayer.rotationYaw * 0.01745329F - 3.141593F);
        float sinYaw = MathHelper.sin(-entityplayer.rotationYaw * 0.01745329F - 3.141593F);
        float cosPitch = -MathHelper.cos(-entityplayer.rotationPitch * 0.01745329F);
        float sinPitch = MathHelper.sin(-entityplayer.rotationPitch * 0.01745329F);
        double length = -5D;
        Vec3 posVec = Vec3.createVectorHelper(entityplayer.posX, entityplayer.posY + 1.62D - (double)entityplayer.yOffset, entityplayer.posZ);        
        Vec3 lookVec = posVec.addVector(sinYaw * cosPitch * length, sinPitch * length, cosYaw * cosPitch * length);
        
        if(world.isRemote)
        {
        	world.spawnEntityInWorld(new EntityDebugVector(world, new Vector3f(posVec), new Vector3f(lookVec.subtract(posVec)), 100));
        }
        
        if(type.healDriveables)
        {
			//Iterate over all EntityDriveables
			for(int i = 0; i < world.loadedEntityList.size(); i++)
			{
				Object obj = world.loadedEntityList.get(i);
				if(obj instanceof EntityDriveable)
				{
					EntityDriveable driveable = (EntityDriveable)obj;
					//Raytrace
					DriveablePart part = driveable.raytraceParts(new Vector3f(posVec), new Vector3f(lookVec.subtract(posVec)));
					//If we hit something that is healable
					if(part != null && part.maxHealth > 0)
					{
						//If its broken and the tool is inifinite or has durability left
						if(part.health < part.maxHealth && (type.toolLife == 0 || itemstack.getItemDamage() < itemstack.getMaxDamage()))
						{
							//Heal it
							part.health += type.healAmount;
							//If it is over full health, cap it
							if(part.health > part.maxHealth)
								part.health = part.maxHealth;
							//If not in creative and the tool should decay, damage it
							if(!entityplayer.capabilities.isCreativeMode && type.toolLife > 0)
								itemstack.setItemDamage(itemstack.getItemDamage() + 1);
							//If the tool is damagable and is destroyed upon being used up, then destroy it
							if(type.toolLife > 0 && type.destroyOnEmpty && itemstack.getItemDamage() == itemstack.getMaxDamage())
								itemstack = null;
							//Our work here is done. Let's be off
							return itemstack;
						}
					}
				}
			}
        }

        if(type.healPlayers)
        {
	        MovingObjectPosition hit = world.clip(posVec, lookVec, true);
	        EntityLivingBase hitLiving = null;
	        
	        //Result check
	        //If we hit nothing, heal the player
	        if(hit == null)
	        {
	            hitLiving = entityplayer;
	        }
	        //If we hit a living entity, heal it
	        else if(hit.typeOfHit == EnumMovingObjectType.ENTITY)
	        {
	        	if(hit.entityHit instanceof EntityLivingBase)
	        	{
	        		hitLiving = (EntityLivingBase)hit.entityHit;
	            }
	        }
	        //Now heal whatever it was we just decided to heal
	        if(hitLiving != null)
	        {        		
	        	hitLiving.heal(type.healAmount);
				//If not in creative and the tool should decay, damage it
				if(!entityplayer.capabilities.isCreativeMode && type.toolLife > 0)
					itemstack.setItemDamage(itemstack.getItemDamage() + 1);
				//If the tool is damagable and is destroyed upon being used up, then destroy it
				if(type.toolLife > 0 && type.destroyOnEmpty && itemstack.getItemDamage() == itemstack.getMaxDamage())
					itemstack = null;
	        }
        }
        return itemstack;
    }
}
